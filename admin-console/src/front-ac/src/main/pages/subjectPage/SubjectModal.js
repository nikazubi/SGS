import FormModal from "../../../components/modal/FormModal";
import useCreateSubject from "./useCreateSubject";
import useUpdateSubject from "./useUpdateSubject";
import SubjectForm from "./SubjectForm";
import {ModalOpenMode} from "../../../utils/constants";


const initialValues = {
    name: "",
    teacher: ""
};

const SubjectModal = ({open, subject, onClose, modalOpenMode, submitButton, ...props}) => {
    const {mutate: onCreate} = useCreateSubject();
    const {mutate: onUpdate} = useUpdateSubject();

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={250}
            maxWidth={700}
            maxHeight={250}
            initialValues={

                {
                    ...initialValues,
                    ...subject,
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={SubjectForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={modalOpenMode === ModalOpenMode.add ? onCreate : onUpdate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default SubjectModal;